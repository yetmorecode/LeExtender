package yetmorecode.LeExtender;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import yetmorecode.format.lx.LxFixupRecord;
import yetmorecode.format.lx.LeObjectPageTableEntry;
import yetmorecode.format.lx.LxHeader;
import yetmorecode.format.lx.ObjectTableEntry;

public class LeReader extends BinaryReader {
	public void readLeHeader(LxHeader header, FileInputStream input) throws IOException {
		// 0x0
		header.signature = readShort(input);
		header.byteOrdering = readByte(input);
		header.wordOrdering = readByte(input);
		header.formatLevel = readInt(input);
		header.cpuType = readShort(input);
		header.osType = readShort(input);
		header.moduleVersion = readInt(input);
		header.moduleFlags = readInt(input);
		header.pageCount = readInt(input);
		header.eipObject = readInt(input);
		header.eip = readInt(input);
		header.espObject = readInt(input);
		header.esp = readInt(input);
		header.pageSize = readInt(input);
		header.lastPageSize = readInt(input);
		
		// 0x30
		header.fixupSectionSize = readInt(input);
		header.fixupSectionChecksum = readInt(input);
		header.loaderSectionSize = readInt(input);
		header.loaderSectionChecksum = readInt(input);
		header.objectTableOffset = readInt(input);
		header.objectCount = readInt(input);
		header.pageTableOffset = readInt(input);
		header.iterPagesOffset = readInt(input);
		header.resourceTableOffset = readInt(input);
		header.resourceCount = readInt(input);
		header.residentNameTableOffset = readInt(input);
		header.entryTableOffset = readInt(input);
		
		// 0x60
		header.directivesTableOffset = readInt(input);
		header.directivesCount = readInt(input);
		header.fixupPageTableOffset = readInt(input);
		header.fixupRecordTableOffset = readInt(input);
		header.importModuleNameTableOffset = readInt(input);
		header.importModuleNameCount = readInt(input);
		header.importProcedureNameTableOffset = readInt(input);
		header.checksumTableOffset = readInt(input);
		header.dataPagesOffset = readInt(input);
		header.preloadPagesCount = readInt(input);
		header.nameTableOffset = readInt(input);
		header.nameTableLength = readInt(input);
		
		// 0x90
		header.nameTableChecksum = readInt(input);
		header.autoDataSegmentObjectNumber = readInt(input);
		header.debugOffset = readInt(input);
		header.debugLength = readInt(input);
		header.pagesInPreloadSectionCount = readInt(input);
		header.pagesInDemandSectionCount = readInt(input);
		header.heapSize = readInt(input);
		header.stackSize = readInt(input);
	}
	
	public void readObjectTableEntry(ObjectTableEntry entry, FileInputStream input) throws IOException {
		entry.size = readInt(input);
		entry.base = readInt(input);
		entry.flags = readInt(input);
		entry.pageTableIndex = readInt(input);
		entry.pageCount = readInt(input);
		input.skip(4);
	}
	
	public void readObjectPageTableEntry(LeObjectPageTableEntry entry, FileInputStream input) throws IOException {
		byte[] bytes = new byte[4];
		bytes[0] = 0;
		bytes[1] = readByte(input);
		bytes[2] = readByte(input);
		bytes[3] = readByte(input);
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.BIG_ENDIAN);
		entry.dataOffset = bb.getInt();
		entry.flags  = readByte(input);
	}
	
	public void readFixupRecord(LxFixupRecord fixup, FileInputStream input) throws IOException {
		fixup.sourceType = readByte(input);
		fixup.targetFlags = readByte(input);
		
		// Source offset or source list length 
		if ((fixup.sourceType & LxFixupRecord.SOURCE_SOURCE_LIST) > 0) {
			fixup.sourceOffset = readByte(input);
		} else {
			fixup.sourceOffset = readShort(input);
		}
		
		// Target data
		switch (fixup.sourceType & LxFixupRecord.SOURCE_MASK) {
		case LxFixupRecord.SOURCE_32BIT_OFFSET_FIXUP:
			if ((fixup.targetFlags & LxFixupRecord.TARGET_16BIT_OBJECT) > 0) {
				fixup.objectNumber = readShort(input);
			} else {
				fixup.objectNumber = readByte(input);
			}
			
			if ((fixup.targetFlags & LxFixupRecord.TARGET_32BIT_OFFSET) > 0) {
				fixup.targetOffset = readInt(input);
			} else {
				fixup.targetOffset = readShort(input);
			}
			
			break;
		case LxFixupRecord.SOURCE_16BIT_SELECTOR_FIXUP:
			if ((fixup.targetFlags & LxFixupRecord.TARGET_16BIT_OBJECT) > 0) {
				fixup.objectNumber = readShort(input);
			} else {
				fixup.objectNumber = readByte(input);
			}
			
			break;
		default:
		}
		
		// Read source list if needed
		if ((fixup.sourceType & LxFixupRecord.SOURCE_SOURCE_LIST) > 0) {
			for (int i = 0; i < fixup.sourceOffset; i++) {
				short off = readShort(input);
				fixup.sourceList.add(off);
			}
			
		}
	}
}
