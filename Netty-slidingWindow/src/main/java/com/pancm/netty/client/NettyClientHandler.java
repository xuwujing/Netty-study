package com.pancm.netty.client;

import java.util.Date;

import com.pancm.netty.pojo.Message;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 
* @Title: NettyClientHandler
* @Description: 客户端业务逻辑实现
* @Version:1.0.0  
* @author pancm
* @date 2017年10月8日
 */
public class NettyClientHandler extends  ChannelInboundHandlerAdapter {
 

 

    /** 发送次数 */
    private int count = 1;  

    /**循环次数 */ 
    private int fcount = 1;  
    
    
    /** 心跳指令 */
	private final String CMD_HEART = "cmd_heart";
    /** 登录指令 */
	private final String CMD_LOGIN = "cmd_login";
     /**
     * 建立连接时
     */
    @Override  
    public void channelActive(ChannelHandlerContext ctx) throws Exception {  
        System.out.println("建立连接时："+new Date());  
        ctx.fireChannelActive();  
        Message message = new Message();
    	message.setCmd(CMD_LOGIN);
    	message.setId(1);
    	message.setMsg("请求登录!");
    	ctx.writeAndFlush(message.toString());
    }  

     /**
      * 关闭连接时
      */
    @Override  
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {  
        System.out.println("关闭连接时："+new Date());  
    	final EventLoop eventLoop = ctx.channel().eventLoop();  
    	NettyClient.nettyClient.doConnect(new Bootstrap(), eventLoop);  
		super.channelInactive(ctx); 
    }  

    /**
     * 心跳请求处理
     * 每4秒发送一次心跳请求;
     * 
     */
    @Override  
    public void userEventTriggered(ChannelHandlerContext ctx, Object obj) throws Exception {  
        System.out.println("循环请求的时间："+new Date()+"，次数"+fcount);  
        if (obj instanceof IdleStateEvent) {   
            IdleStateEvent event = (IdleStateEvent) obj;  
            //如果写通道处于空闲状态,就发送心跳命令
            if (IdleState.WRITER_IDLE.equals(event.state())) {  
            	Message message = new Message();
            	message.setCmd(CMD_HEART);
                ctx.channel().writeAndFlush(message.toString());  
                fcount++;
            }  
        }  
    }  

    /**
     * 业务逻辑处理   
     */
    @Override  
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {  
        System.out.println("第"+count+"次"+",客户端接受的消息:"+msg);
        count++;
    }  
}
