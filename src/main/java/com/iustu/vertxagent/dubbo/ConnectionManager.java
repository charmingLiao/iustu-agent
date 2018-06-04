package com.iustu.vertxagent.dubbo;

import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.ArrayList;
import java.util.List;

public class ConnectionManager implements Connection.OnConnectionListener {


    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private final int nConns = 4;

    private int connIndex = 0;

    private final Object lock = new Object();
    private final List<Connection> conns = new ArrayList<>();
    private Connection pConn;

    public ConnectionManager() {

    }

    /**
     * 所有方法都不安全，全部加了锁
     *
     * @return
     */
    public ChannelFuture getChannelFuture() {
//        try {
//            if (conns.size() == nConns) {
//                return conns.get(connIndex++ % nConns).connectChannel();
//            }
//        } catch (IndexOutOfBoundsException e) {
//            e.printStackTrace();
//        }

        synchronized (lock) {
            final int size = conns.size();
            if (size == 0) {
                if (pConn == null) {
                    pConn = new Connection(eventLoopGroup, this);
                }

                return pConn.connectChannel();
            } else {
                if (size < nConns && pConn == null) {
                    pConn = new Connection(eventLoopGroup, this);
                    pConn.connectChannel();
                }

                return conns.get(connIndex++ % size).connectChannel();
            }
        }
    }

    @Override
    public void onConnectionConnected(Connection conn) {
        synchronized (lock) {
            if (pConn == conn) {
                pConn = null;
            }

            conns.add(conn);
        }
    }

    @Override
    public void onConnectionConnectFailed(Connection conn) {
        synchronized (lock) {
            if (pConn == conn) {
                pConn = null;
            }
        }
    }

    @Override
    public void onConnectionClosed(Connection conn) {
        synchronized (lock) {
            conns.remove(conn);
        }
    }
}
