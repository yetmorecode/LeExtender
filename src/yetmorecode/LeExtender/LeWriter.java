package yetmorecode.LeExtender;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import yetmorecode.format.lx.LeObjectPageTableEntry;
import yetmorecode.format.lx.LxFixupRecord;
import yetmorecode.format.lx.LxHeader;
import yetmorecode.format.lx.ObjectTableEntry;

public class LeWriter {
	public void writeByte(byte data, FileOutputStream output) throws IOException {
		output.write(data);
	}
	
	public void writeShort(short data, FileOutputStream output) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putShort(data);
		output.write(bb.array());
	}
	
	public void writeInt(int data, FileOutputStream output) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(data);
		output.write(bb.array());
	}
	
	public void writeLxHeader(LxHeader header, FileOutputStream output) throws IOException {
		writeShort(header.signature, output);
		writeByte(header.byteOrdering, output);
		writeByte(header.wordOrdering, output);
		writeInt(header.formatLevel, output);
		writeShort(header.cpuType, output);
		writeShort(header.osType, output);
		writeInt(header.moduleVersion, output);
		writeInt(header.moduleFlags, output);
		writeInt(header.pageCount, output);
		writeInt(header.eipObject, output);
		writeInt(header.eip, output);
		writeInt(header.espObject, output);
		writeInt(header.esp, output);
		writeInt(header.pageSize, output);
		writeInt(header.lastPageSize, output);
		
		// 0x30
		writeInt(header.fixupSectionSize, output);
		writeInt(header.fixupSectionChecksum, output);
		writeInt(header.loaderSectionSize, output);
		writeInt(header.loaderSectionChecksum, output);
		writeInt(header.objectTableOffset, output);
		writeInt(header.objectCount, output);
		writeInt(header.pageTableOffset, output);
		writeInt(header.iterPagesOffset, output);
		writeInt(header.resourceTableOffset, output);
		writeInt(header.resourceCount, output);
		writeInt(header.residentNameTableOffset, output);
		writeInt(header.entryTableOffset, output);
		
		// 0x60
		writeInt(header.directivesTableOffset, output);
		writeInt(header.directivesCount, output);
		writeInt(header.fixupPageTableOffset, output);
		writeInt(header.fixupRecordTableOffset, output);
		writeInt(header.importModuleNameTableOffset, output);
		writeInt(header.importModuleNameCount, output);
		writeInt(header.importProcedureNameTableOffset, output);
		writeInt(header.checksumTableOffset, output);
		writeInt(header.dataPagesOffset, output);
		writeInt(header.preloadPagesCount, output);
		writeInt(header.nameTableOffset, output);
		writeInt(header.nameTableLength, output);
		
		// 0x90
		writeInt(header.nameTableChecksum, output);
		writeInt(header.autoDataSegmentObjectNumber, output);
		writeInt(header.debugOffset, output);
		writeInt(header.debugLength, output);
		writeInt(header.pagesInPreloadSectionCount, output);
		writeInt(header.pagesInDemandSectionCount, output);
		writeInt(header.heapSize, output);
		writeInt(header.stackSize, output);
	}
	
	public void writeObjectTableEntry(ObjectTableEntry entry, FileOutputStream output) throws IOException {
		writeInt(entry.size, output);
		writeInt(entry.base, output);
		writeInt(entry.flags, output);
		writeInt(entry.pageTableIndex, output);
		writeInt(entry.pageCount, output);
		writeInt(0, output);
	}
	
	public void writeObjectPageTableEntry(LeObjectPageTableEntry entry, FileOutputStream output) throws IOException {
		output.write((entry.getOffset() & 0xff0000) >> 16);
		output.write((entry.getOffset() & 0xff00) >> 8);
		output.write(entry.getOffset() & 0xff);
		output.write(entry.flags);
	}
	
	public void writeFixupRecord(LxFixupRecord fixup, FileOutputStream output) throws IOException {
		writeByte(fixup.sourceType, output);
		writeByte(fixup.targetFlags, output);
		
		// Source offset or source list length 
		if ((fixup.sourceType & LxFixupRecord.SOURCE_SOURCE_LIST) > 0) {
			writeByte((byte) fixup.sourceOffset, output);
		} else {
			writeShort(fixup.sourceOffset, output);
		}
		
		// Target data
		switch (fixup.sourceType & LxFixupRecord.SOURCE_MASK) {
		case LxFixupRecord.SOURCE_32BIT_OFFSET_FIXUP:
			if ((fixup.targetFlags & LxFixupRecord.TARGET_16BIT_OBJECT) > 0) {
				writeShort(fixup.objectNumber, output);
			} else {
				writeByte((byte)fixup.objectNumber, output);
			}
			
			if ((fixup.targetFlags & LxFixupRecord.TARGET_32BIT_OFFSET) > 0) {
				writeInt(fixup.targetOffset, output);
			} else {
				writeShort((short)fixup.targetOffset, output);
			}
			
			break;
		case LxFixupRecord.SOURCE_16BIT_SELECTOR_FIXUP:
			if ((fixup.targetFlags & LxFixupRecord.TARGET_16BIT_OBJECT) > 0) {
				writeShort(fixup.objectNumber, output);
			} else {
				writeByte((byte) fixup.objectNumber, output);
			}
			
			break;
		default:
		}
		
		// Read source list if needed
		if ((fixup.sourceType & LxFixupRecord.SOURCE_SOURCE_LIST) > 0) {
			for (int i = 0; i < fixup.sourceOffset; i++) {
				writeShort((short) 0, output);
			}
			
		}
	}
}
