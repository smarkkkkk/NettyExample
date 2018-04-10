package udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class NettyUdpServer{

	public static void main(String[] args){
		
		int port = 9033;
		new NettyUdpServer().run(port);		
	}


	public void run(int port) {
		EventLoopGroup group = new NioEventLoopGroup();
		Bootstrap bootsrap = new Bootstrap();
		try{
			bootsrap.group(group).channel(NioDatagramChannel.class)
				.handler(new ServerHandler());
			ChannelFuture future = bootsrap.bind(port).sync();
			future.channel().closeFuture().await();
		
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			group.shutdownGracefully();
		}
		
		
	}


}

class ServerHandler extends SimpleChannelInboundHandler<DatagramPacket>{

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
		ByteBuf byteBuf = packet.content();
		byte[] bytes = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(bytes);
		String body = getHexString(bytes);
		System.out.println(body);
		getBleJson(body);
		
	}
	
	public static String getHexString(byte[] b) throws Exception {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
	
	public static void getBleJson(String data){
		String head = data.substring(0,4);
		if(!"abef".equals(head)){
			System.out.println("不合法的包头");
			return;
		}
		String lengthString = data.substring(4,6);
		int length = Integer.parseInt(lengthString, 16)*2;
		int bleLength = length - 12*2;
		int bleCount = bleLength/10;
		String msgCtrlByte = data.substring(6,8);
		if(!"01".equals(msgCtrlByte)){
			System.out.println("不合法的帧控制字，此包不是数据请求包");
			return;
		}
		String cardId = data.substring(12,14)+data.substring(10,12);
		String buttonStatus = data.substring(length+6-10, length+6-8);
		String wakeStatus = data.substring(length+6-8, length+6-6);
		
		System.out.println("cardId:"+cardId);
		System.out.println("buttonStatus:"+buttonStatus);
		System.out.println("wakeStatus:"+wakeStatus);

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
		ctx.close();
		cause.printStackTrace();
	}
	
	
}