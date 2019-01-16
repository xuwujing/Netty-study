package com.pancm.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutor;

/**
 * 
 * @Title: NettyServer
 * @Description: Netty服务端 滑动窗口 测试
 * @Version:1.0.0
 * @author pancm
 * @date 2019年1月16日
 */
public class NettyServer {
	private static final int port = 9876; // 设置服务端端口
	// 创建一个boss和work线程
	private EventLoopGroup boss = null;
	private EventLoopGroup work = null;
	private ServerBootstrap b = null;

	// 定义一个通道
	private ChannelGroup channels;
	private ChannelFuture f;
	// 定义通道组的共用定时器任务
	private EventExecutor executor = new DefaultEventExecutor(new DefaultThreadFactory("netty-server-threadFactory"));

 
	
	public NettyServer() {
		// 为boss组指定一个线程
		boss = new NioEventLoopGroup(1, new DefaultThreadFactory("netty-server-boss"));
		// 为work组指定当前系统cup个数乘以2的线程数
		work = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
				new DefaultThreadFactory("netty-server-work"));
		channels = new DefaultChannelGroup(executor);
	}

	public void run() {
		try {
			//初始化
			init();
			// 服务器绑定端口监听
			f = b.bind(port).sync();
			System.out.println("服务端启动成功,端口是:" + port);
			// 监听服务器关闭监听
			f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			destroy();
		}
	}

	/**
	 * 初始化
	 */
	private void init() {
		if (null != b) {
			return;
		}
		b = new ServerBootstrap();
		b.group(boss, work);
		b.channel(NioServerSocketChannel.class);
		//
		int backlog = 128;
		//设置缓冲区队列
		b.option(ChannelOption.SO_BACKLOG, backlog)
		 //设置是否可以重用端口
		 .option(ChannelOption.SO_REUSEADDR, false);
		//设置无延时
		b.childOption(ChannelOption.TCP_NODELAY, true)
		//设置套接字发送缓冲区大小
		.childOption(ChannelOption.SO_SNDBUF, 1000 * 1024 * 1024);
		// 设置过滤器
		b.childHandler(new NettyServerFilter());

	}

	/*
	 * 释放资源
	 */
	public void destroy() {
		if (null != channels) {
			channels.close().awaitUninterruptibly();
		}
		if (null != work) {
			work.shutdownGracefully();
		}
		if (null != boss) {
			boss.shutdownGracefully();
		}
	}

	/**
	 * Netty创建全部都是实现自AbstractBootstrap。 客户端的是Bootstrap，服务端的则是 ServerBootstrap。
	 **/
	public static void main(String[] args) {
		NettyServer nettyServer = new NettyServer();
		nettyServer.run();
	}

}
