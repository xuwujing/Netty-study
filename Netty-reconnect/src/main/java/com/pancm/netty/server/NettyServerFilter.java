package com.pancm.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
  * 
 * Title: HelloServerInitializer
 * Description: Netty 服务端过滤器
 * Version:1.0.0  
 * @author pancm
 * @date 2017年10月8日
  */
public class NettyServerFilter extends ChannelInitializer<SocketChannel> {

     @Override
     protected void initChannel(SocketChannel ch) throws Exception {
         ChannelPipeline ph = ch.pipeline();
         // 以("\n")为结尾分割的 解码器
//        ph.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
         // 解码和编码，应和客户端一致
         //入参说明: 读超时时间、写超时时间、所有类型的超时时间、时间格式
         ph.addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
         ph.addLast("decoder", new StringDecoder());
         ph.addLast("encoder", new StringEncoder());
         ph.addLast("handler", new NettyServerHandler());// 服务端业务逻辑
     }
 }
