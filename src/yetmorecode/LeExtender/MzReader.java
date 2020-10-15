package yetmorecode.LeExtender;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import yetmorecode.format.mz.MzHeader;

public class MzReader extends BinaryReader {
	
	public void readMzHeader(MzHeader header, FileInputStream input)throws IOException {
		
		input.getChannel().position(0);
		header.signature = readShort(input);
		header.bytesOnLastBlock = readShort(input);
		header.blockCount = readShort(input);
		header.relocations = readShort(input);
		header.headerSize = readShort(input);
		header.minExtraParagraphs = readShort(input);
		header.maxExtraParagraphs = readShort(input);
		header.ss = readShort(input);
		header.sp = readShort(input);
		header.checksum = readShort(input);
		header.ip = readShort(input);
		header.cs = readShort(input);
		header.relocationTableOffset = readShort(input);
		header.overlayNumber = readShort(input);
		input.skip(8);
		header.oemId = readShort(input);
		header.oemInfo = readShort(input);
		input.skip(20);
		header.fileAddressNewExe = readInt(input);		
	}
	

	
}
