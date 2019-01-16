package com.pancm.netty.server;

import com.alibaba.fastjson.JSON;
import com.cloudhopper.commons.util.windowing.DuplicateKeyException;
import com.cloudhopper.commons.util.windowing.OfferTimeoutException;
import com.cloudhopper.commons.util.windowing.Window;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.pancm.netty.pojo.Message;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 
 * @Title: NettyServerHandler
 * @Description: 服务端业务逻辑
 * @Version:1.0.0
 * @author pancm
 * @date 2017年10月8日
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

	/** 空闲次数 */
	private int idle_count = 1;
	/** 发送次数 */
	private int count = 1;

	/** 心跳指令 */
	private final String CMD_HEART = "cmd_heart";
	/** 登录指令 */
	private final String CMD_LOGIN = "cmd_login";
	/** 成功 */
	private final String CMD_SUC = "ok";
	
	boolean flag=true;
	
	// 滑动窗口
	private Window<Integer, Message, Message> window = new Window<Integer, Message, Message>(20);

	/**
	 * 超时处理 如果5秒没有接受客户端的心跳，就触发; 如果超过两次，则直接关闭;
	 */
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object obj) throws Exception {
		if (obj instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) obj;
			if (IdleState.READER_IDLE.equals(event.state())) { // 如果读通道处于空闲状态，说明没有接收到心跳命令
				System.out.println("已经5秒没有接收到客户端的信息了");
				if (idle_count > 1) {
					System.out.println("关闭这个不活跃的channel");
					ctx.channel().close();
				}
				idle_count++;
			}
		} else {
			super.userEventTriggered(ctx, obj);
		}
	}

	/**
	 * 业务逻辑处理
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
		System.out.println("第" + count + "次" + ",服务端接受的消息:" + obj);
		Message msg = JSON.parseObject(obj.toString(), Message.class);
		String cmd = msg.getCmd();
		// 如果是心跳命令，则发送给客户端
		if (CMD_HEART.equals(cmd)) {
			msg.setCmd(CMD_SUC);
			msg.setMsg("服务端成功收到请求!");
			ctx.writeAndFlush(msg.toString());
		} else if (CMD_LOGIN.equals(cmd)) {
			handlerResponse(msg);
			msg.setCmd(CMD_SUC);
			msg.setMsg("登录成功！");
			ctx.writeAndFlush(msg.toString());
			
		} else {
			System.out.println("未知命令!" + cmd);
			return;
		}
		if(flag) {
			msg.setCmd(CMD_SUC);
			msg.setMsg("滑动窗口测试!");
			new Thread(()->handlerRequest(msg,ctx)).start();
			flag=false;
		}
		count++;
	}

	/**
	 * 异常处理
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	/**
	 * 滑动窗口响应缓存
	 */
	public void handlerResponse(Message msg) {
		int id = msg.getId();
		WindowFuture<Integer, Message, Message> future = null;
		try {

			future = window.complete(id, msg);
		} catch (InterruptedException e) {
			System.err.println("完成窗口失败！ID:" + id + " 异常:" + e);
			return;
		}
		if (null == future) {
			System.err.println("完成窗口在完成方法引用前失败!id:"+id);
			return;
		}
		if (future.isSuccess()) {
			System.err.println("等待响应的请求:" + future.getRequest());
			return;
		} else {
			System.err.println("等待超时的请求:" + future.getRequest());
			return;
		}

	}

	/*
	 * 滑动窗口响应请求
	 */
	@SuppressWarnings("unchecked")
	public Message handlerRequest(Message request, ChannelHandlerContext ctx) {
		// 入滑动窗口超时、滑动窗口内超时、客户端调用超时 毫秒
		int windowWaitOffset = 30000;
		int windowExpiration = 30000;
		int invokeOffset = 30000;
		int id = request.getId();
		WindowFuture<Integer, Message, Message> reqFuture = null;
		try {
			reqFuture = window.offer(id, request, windowWaitOffset, windowExpiration);
		} catch (DuplicateKeyException e) {
			System.out.println("重复调用！" + e);
			return null;
		} catch (OfferTimeoutException e) {
			System.out.println("滑动窗口请求超时！" + e);
			return null;
		} catch (InterruptedException e) {
			System.out.println("滑动窗口过程中被中断!" + e);
			return null;
		}
		System.out.println("滑动窗口发送的数据:"+request.toString());
		ChannelFuture future = ctx.writeAndFlush(request.toString());
		future.awaitUninterruptibly();
		if (future.isCancelled()) {// 被中断
			try {
				// 取消该id的数据
				window.cancel(id);
			} catch (InterruptedException e) {
				// IGNORE
			}
			System.out.println("调用失败，被用户中断!");
		} else if (!future.isSuccess()) {// 失败，遇到异常
			try {
				window.cancel(id);
			} catch (InterruptedException e) {
			}
			Throwable cause = future.cause();
			System.out.println("无法将数据正确写到Channel,Channel可能被关闭" + cause);
		} else {// 正常
			try {
				reqFuture.await(invokeOffset);
			} catch (InterruptedException e) {
				System.out.println("调用过程中被用户中断!-->消息可能已经发送到服务端" + e);
			}
			// 成功返回正确的响应
			if (reqFuture.isSuccess()) {
				return reqFuture.getResponse();
			}
			try {
				window.cancel(id);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		return null;
	}

}
