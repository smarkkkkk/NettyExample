package tcp;

import java.util.Date;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;



public class NettyNIOServer {
	
	public void bind(int port) throws Exception{
		
		//包含NIO线程，本质是Reactor线程组，1个用来接受客户端连接，另1个进行SocketChannel的网络读写
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try{
			ServerBootstrap bootstrap = new ServerBootstrap();//辅助启动类
			//配置bootstrap
			bootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)//设置Channel对象
				.option(ChannelOption.SO_BACKLOG, 1024)//配置backlog
				.childHandler(new ChildChannelHandler());//配置进行消息处理的handler
			
			//异步连接
			ChannelFuture future = bootstrap.bind(port).sync();
			
			future.channel().closeFuture().sync();
			
		}catch (Exception e) {
			e.printStackTrace();
		}finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
	//IO事件处理类
	private class ChildChannelHandler extends ChannelInitializer<SocketChannel>{

		/**
		 * Netty为每个channel建立一个pipeline
		 */
		@Override
		protected void initChannel(SocketChannel arg0) throws Exception {
			//按行解码，防止TCP粘包或拆包，但是发送消息时末尾要加换行符
			arg0.pipeline().addLast(new LineBasedFrameDecoder(1024));
			//String解码，channelRead中的msg可以直接转为String类型
			arg0.pipeline().addLast(new StringDecoder());
			//加入服务器处理器
			arg0.pipeline().addLast(new ServerHandler());

		}

	}
	
	public static void main(String[] args){
		
		int port = 9034;
		try {
			new NettyNIOServer().bind(port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}


class ServerHandler extends ChannelHandlerAdapter{
	
	@Override
	public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception{
//		ByteBuf buf = (ByteBuf)msg;
//		byte[] reqest = new byte[buf.readableBytes()];
//		buf.readBytes(reqest);
//		String body = new String(reqest,"utf-8");
//		System.out.println(new Date(System.currentTimeMillis())+" "+body);
		String body = (String)msg;
		System.out.println(new Date(System.currentTimeMillis())+" "+body);
		
		String response = "Message has been Accepted!"+System.getProperty("line.separator");
		ByteBuf resp = Unpooled.copiedBuffer(response.getBytes());
		//ctx.writeAndFlush(resp);
		ctx.write(resp);
		
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception{
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
		ctx.close();
	}
	
}
