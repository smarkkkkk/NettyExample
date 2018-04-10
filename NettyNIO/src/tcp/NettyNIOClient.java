package tcp;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class NettyNIOClient {
	
	public void connect(String host, int port){
		
		NioEventLoopGroup group = new NioEventLoopGroup();
		try{
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.TCP_NODELAY, true)
			.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel arg0) throws Exception {
					arg0.pipeline().addLast(new LineBasedFrameDecoder(1024));
					arg0.pipeline().addLast(new StringDecoder());
					arg0.pipeline().addLast(new ClientHandler());
					
				}
			});
		
		//异步连接
		ChannelFuture future = bootstrap.connect(host,port).sync();
		future.channel().closeFuture().sync();
		
		}catch (Exception e) {
			e.printStackTrace();
		}finally{
			group.shutdownGracefully();
		}
		
	}
	
	public static void main(String[] args){
		while(true)
		new NettyNIOClient().connect("127.0.0.1", 9034);
	}

}

class ClientHandler extends ChannelHandlerAdapter{
	
	private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
	//private final ByteBuf firstMessage;
	byte[] request;
	int count = 0;
	ByteBuf firstMessage;
	public ClientHandler() {
		request = ("Client starts!"+System.getProperty("line.separator")).getBytes();
		firstMessage = Unpooled.buffer(request.length);
		firstMessage.writeBytes(request);

	}
	/**
	 * channelActive方法，在连接建立成功时调用
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx){

		ctx.writeAndFlush(firstMessage);


	}
	
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
//		ByteBuf buf = (ByteBuf)msg;
//		byte[] bytes = new byte[buf.readableBytes()];
//		buf.readBytes(bytes);
//		String body = new String(bytes,"utf-8");
		
		String body = (String)msg;
		System.out.println(body+" count:" + ++count);

	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx){
		ctx.close();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
		LOGGER.warning("异常："+cause.getMessage());
		ctx.close();
	}
}


