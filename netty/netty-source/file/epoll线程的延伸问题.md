
epoll相关的主要两个方法是epoll_wait和epoll_ctl，多线程同时操作同一个epoll实例，那么首先需要确认epoll相关方法是否线程安全：简单来说，epoll是通过锁来保证线程安全的, epoll中粒度最小的自旋锁ep->lock(spinlock)用来保护就绪的队列, 互斥锁ep->mtx用来保护epoll的重要数据结构红黑树。

看到这里，可能有的小伙伴想到了Nginx多进程针对监听端口的处理策略，Nginx是通过accept_mutex机制来保证的。accept_mutex是nginx的(新建连接)负载均衡锁，让多个worker进程轮流处理与client的新连接。当某个worker进程的连接数达到worker_connections配置（单个worker进程的最大处理连接数）的最大连接数的7/8时，会大大减小获取该worker获取accept锁的概率，以此实现各worker进程间的连接数的负载均衡。accept锁默认打开，关闭它时nginx处理新建连接耗时会更短，但是worker进程之间可能连接不均衡，并且存在“惊群”问题。只有在使能accept_mutex并且当前系统不支持原子锁时，才会用文件实现accept锁。注意，accept_mutex加锁失败时不会阻塞当前线程，类似tryLock。

现代linux中，多个socker同时监听同一个端口也是可行的，nginx 1.9.1也支持这一行为。linux 3.9以上内核支持SO_REUSEPORT选项，允许多个socker bind/listen在同一端口上。这样，多个进程可以各自申请socker监听同一端口，当连接事件来临时，内核做负载均衡，唤醒监听的其中一个进程来处理，reuseport机制有效的解决了epoll惊群问题。

再回到刚才提出的问题，java中多线程来监听同一个对外端口，epoll方法是线程安全的，这样就可以使用使用多线程监听epoll_wait了么，当然是不建议这样干的，除了epoll的惊群问题之外，还有一个就是，一般开发中我们使用epoll设置的是LT模式（水平触发方式，与之相对的是ET默认，前者只要连接事件未被处理就会在epoll_wait时始终触发，后者只会在真正有事件来时在epoll_wait触发一次），这样的话，多线程epoll_wait时就会导致第一个线程epoll_wait之后还未处理完毕已发生的事件时，第二个线程也会epoll_wait返回，显然这不是我们想要的，关于java nio的测试demo如下：

```java
public class NioDemo {
    private static AtomicBoolean flag = new AtomicBoolean(true);
    public static void main(String[] args) throws Exception {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(8080));
        // non-block io
        serverChannel.configureBlocking(false);
        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 多线程执行
        Runnable task = () -> {
            try {
                while (true) {
                    if (selector.select(0) == 0) {
                        System.out.println("selector.select loop... " + Thread.currentThread().getName());
                        Thread.sleep(1);
                        continue;
                    }

                    if (flag.compareAndSet(true, false)) {
                        System.out.println(Thread.currentThread().getName() + " over");
                        return;
                    }

                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();

                        // accept event
                        if (key.isAcceptable()) {
                            handlerAccept(selector, key);
                        }

                        // socket event
                        if (key.isReadable()) {
                            handlerRead(key);
                        }

                        /**
                         * Selector不会自己从已选择键集中移除SelectionKey实例，必须在处理完通道时手动移除。
                         * 下次该通道变成就绪时，Selector会再次将其放入已选择键集中。
                         */
                        iter.remove();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Thread thread = new Thread(task);
            threadList.add(thread);
            thread.start();
        }
        for (Thread thread : threadList) {
            thread.join();
        }
        System.out.println("main end");
    }

    static void handlerAccept(Selector selector, SelectionKey key) throws Exception {
        System.out.println("coming a new client... " + Thread.currentThread().getName());
        Thread.sleep(10000);
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
    }

    static void handlerRead(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();

        int num = channel.read(buffer);
        if (num <= 0) {
            // error or fin
            System.out.println("close " + channel.getRemoteAddress());
            channel.close();
        } else {
            buffer.flip();
            String recv = Charset.forName("UTF-8").newDecoder().decode(buffer).toString();
            System.out.println("recv: " + recv);

            buffer = ByteBuffer.wrap(("server: " + recv).getBytes());
            channel.write(buffer);
        }
    }
}
```