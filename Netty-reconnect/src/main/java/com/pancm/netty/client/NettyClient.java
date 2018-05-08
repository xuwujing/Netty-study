package com.pancm.netty.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
/**
 * 
* Title: NettyClient
* Description: 
* Netty客户端  心跳测试
* Version:1.0.0  
* @author pancm
* @date 2017年10月8日
 */
public class NettyClient {

    public static String host = "127.0.0.1";  //ip地址
    public static int port = 9876;          //端口
    /// 通过nio方式来接收连接和处理连接   
    private static EventLoopGroup group = new NioEventLoopGroup(); 
    private static  Bootstrap b = new Bootstrap();
    private static ChannelFuture cf;
    private static ChannelFutureListener cfl;
    /**
     * Netty创建全部都是实现自AbstractBootstrap。
     * 客户端的是Bootstrap，服务端的则是    ServerBootstrap。
     **/
    public static void main(String[] args) throws InterruptedException, IOException { 
            System.out.println("客户端成功启动...");
            b.group(group);
            b.channel(NioSocketChannel.class);
            b.handler(new NettyClientFilter()); 
         // 连接服务端
            cf = b.connect(host, port).sync();
//            cf.addListener((ChannelFuture futureListener)->{
//				final EventLoop eventLoop = futureListener.channel().eventLoop();
//				if (!futureListener.isSuccess()) {
//					System.out.println("与服务端断开连接!在10s之后准备尝试重连!");
//					eventLoop.schedule(() -> doConnect(), 10, TimeUnit.SECONDS);
//				}
//			});
            
            cfl = new ChannelFutureListener() {
                public void operationComplete(ChannelFuture f) throws Exception {
                	final EventLoop eventLoop = f.channel().eventLoop();
                    if (f.isSuccess()) {
                        System.out.println("重新连接服务器成功");
                    } else {
                        System.out.println("与服务端断开连接!在3s之后准备尝试重连!");
                        //  3秒后重新连接
                        eventLoop.schedule(() -> doConnect(), 3, TimeUnit.SECONDS);
                    }
                }
            };
            
            star();
    }

    public static void star() throws IOException{
        String str="Hello Netty";
        cf.channel().writeAndFlush(str);
        System.out.println("客户端发送数据:"+str);
   }   
    
    /**
     * 重连
     */
    public static void doConnect(){
    	   ChannelFuture future = null;
           try {
               future = b.connect(host, port);
               future.addListener(cfl);
           } catch (Exception e) {
        	   System.out.println("连接关闭");
               e.printStackTrace();
           }

    }
}
