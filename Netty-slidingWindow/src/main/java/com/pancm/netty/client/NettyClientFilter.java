package com.pancm.netty.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;
/**
 * 
* @Title: NettyClientFilter
* @Description: Netty客户端 过滤器
* @Version:1.0.0  
* @author pancm
* @date 2017年10月8日
 */
public class NettyClientFilter extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline ph = ch.pipeline();
        /*
         * 解码和编码，应和服务端一致
         * */
//        ph.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        //入参说明: 读超时时间、写超时时间、所有类型的超时时间、时间格式
        //因为服务端设置的超时时间是5秒，所以设置4秒
        ph.addLast( new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS));  
        ph.addLast("decoder", new StringDecoder());
        ph.addLast("encoder", new StringEncoder());
        ph.addLast("aggregator", new HttpObjectAggregator(10*1024*1024));
        ph.addLast("handler", new NettyClientHandler()); //客户端的逻辑
    }
}
