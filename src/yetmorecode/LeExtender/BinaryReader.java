package yetmorecode.LeExtender;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BinaryReader {
	protected short readShort(FileInputStream input) throws IOException  {
		byte[] bytes = input.readNBytes(2);
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getShort();
	}

	protected int readInt(FileInputStream input) throws IOException  {
		byte[] bytes = input.readNBytes(4);
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}
	
	protected byte readByte(FileInputStream input) throws IOException {
		byte[] bytes = input.readNBytes(1);
		return bytes[0];
	}
}
