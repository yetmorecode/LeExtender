package yetmorecode.LeExtender;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import yetmorecode.format.lx.LxFixupRecord;
import yetmorecode.format.lx.LxHeader;
import yetmorecode.format.lx.ObjectPageTableEntry;
import yetmorecode.format.lx.LeObjectPageTableEntry;
import yetmorecode.format.lx.LxExecutable;
import yetmorecode.format.lx.ObjectTableEntry;
import yetmorecode.format.mz.MzHeader;

public class LeExtender {
	public static void main(String[] args) {
		String source = "D:\\Games\\f1mod20\\F1.exe";
		String target = "D:\\Games\\f1mod20\\F2.exe";
		
		System.out.println(String.format("Parsing: %s", source));
		try {
			FileInputStream input = new FileInputStream(source);
			LxExecutable exe = new LxExecutable();
			MzReader reader = new MzReader();
			LeReader leReader = new LeReader();
			
			// Read MZ header
			reader.readMzHeader(exe.dosHeader, input);
			System.out.println(String.format("Signature: %x", exe.dosHeader.signature));
			System.out.println(String.format("Header size: %x", exe.dosHeader.headerSize * MzHeader.PARAGRAPH_SIZE));
			System.out.println(String.format("Relocations: %x (%x)", exe.dosHeader.relocations, exe.dosHeader.relocationTableOffset));
			System.out.println(String.format("Blocks: %x", exe.dosHeader.blockCount));
			System.out.println(String.format("CS:IP: %x:%x", exe.dosHeader.cs, exe.dosHeader.ip));
			System.out.println(String.format("SS:SP: %x:%x", exe.dosHeader.ss, exe.dosHeader.sp));
			System.out.println(String.format("Adress new exe: %x", exe.dosHeader.fileAddressNewExe));
			
			// Copy MZ header + DOS code
			
			int leOffset = exe.dosHeader.fileAddressNewExe;
			input.getChannel().position(leOffset);
			
			// Read LX header
			leReader.readLeHeader(exe.header, input);
			System.out.println(String.format("CS:EIP: %x:%x", exe.header.eipObject, exe.header.eip));
			System.out.println(String.format("SS:ESP: %x:%x", exe.header.espObject, exe.header.esp));
			System.out.println(String.format("fixup section size: %x (checksum: %x)", exe.header.fixupSectionSize, exe.header.fixupSectionChecksum));
			System.out.println(String.format("loader section size: %x (checksum: %x)", exe.header.loaderSectionSize, exe.header.loaderSectionChecksum));
			if (exe.header.debugOffset > 0) {
				System.out.println(String.format("debug info offset: %x (%x length)", exe.header.debugOffset, exe.header.debugLength));
			}
			System.out.println(String.format("heap/stack sizes: %x/%x", exe.header.heapSize, exe.header.stackSize));
			System.out.println(String.format("[%x - %x] LE Header", exe.dosHeader.fileAddressNewExe, input.getChannel().position()));
			System.out.println(String.format("[%x - %x] object table (%x objects)", exe.dosHeader.fileAddressNewExe + exe.header.objectTableOffset, exe.dosHeader.fileAddressNewExe + exe.header.objectTableOffset + ObjectTableEntry.SIZE * exe.header.objectCount, exe.header.objectCount));
			System.out.println(String.format("[%x - %x] object pages table (%x pages)", exe.dosHeader.fileAddressNewExe + exe.header.pageTableOffset, exe.dosHeader.fileAddressNewExe + exe.header.pageTableOffset + LeObjectPageTableEntry.SIZE * exe.header.pageCount, exe.header.pageCount));
			System.out.println(String.format("[%x - XXX] resource table (%x resources)", exe.dosHeader.fileAddressNewExe + exe.header.resourceTableOffset, exe.header.resourceCount));
			System.out.println(String.format("[%x - XXX] resident name table", exe.dosHeader.fileAddressNewExe + exe.header.residentNameTableOffset));
			System.out.println(String.format("[%x - XXX] entry table", exe.dosHeader.fileAddressNewExe + exe.header.entryTableOffset));
			if (exe.header.directivesTableOffset > 0) {
				System.out.println(String.format("[%x - XXX] directives", exe.dosHeader.fileAddressNewExe + exe.header.directivesTableOffset));	
			}
			if (exe.header.checksumTableOffset > 0) {
				System.out.println(String.format("[%x - XXX] page checksum table", exe.dosHeader.fileAddressNewExe + exe.header.checksumTableOffset));	
			}
			System.out.println(String.format("[%x - XXX] fixup page table", exe.dosHeader.fileAddressNewExe + exe.header.fixupPageTableOffset));
			
			// read object map
			int objectTableOffset = exe.dosHeader.fileAddressNewExe + exe.header.objectTableOffset;
			input.getChannel().position(objectTableOffset);
			for (int i = 0; i < exe.header.objectCount; i++) {
				ObjectTableEntry entry = new ObjectTableEntry();
				entry.number = i+1;
				leReader.readObjectTableEntry(entry, input);
				exe.objectTable.add(i, entry);
				
				System.out.println(String.format("object %x: %x pages, %x index", entry.number, entry.pageCount, entry.pageTableIndex));
				
			}
			
			// read pages
			int fixupTotal = 0;
			int pageDataOffset = exe.header.dataPagesOffset;
			for (int i = 0; i < exe.header.pageCount; i++) {
				// object page table
				LeObjectPageTableEntry entry = new LeObjectPageTableEntry();
				input.getChannel().position(exe.dosHeader.fileAddressNewExe + exe.header.pageTableOffset + i * LeObjectPageTableEntry.SIZE);
				leReader.readObjectPageTableEntry(entry, input);
				exe.objectPageTable.add(i, entry);
				
				// pages
				int pageSize = exe.header.pageSize;
				if (entry.getOffset() == exe.header.pageCount) {
					pageSize = exe.header.lastPageSize;
				}
				
				input.getChannel().position(pageDataOffset + i * exe.header.pageSize);
				byte[] pageBuffer = input.readNBytes(pageSize);
				exe.pages.add(i, pageBuffer);
				
				input.getChannel().position(exe.dosHeader.fileAddressNewExe + exe.header.fixupPageTableOffset + i * 4);
				int fixupBegin = leReader.readInt(input);
				int fixupEnd = leReader.readInt(input);
				int fixupSize = fixupEnd - fixupBegin;
				
				// fixup page table
				System.out.println(String.format(
					"%x:%x: %x - %x; %x: %x - %x", 
					i, entry.getOffset(), 
					pageDataOffset + i * exe.header.pageSize, pageSize,
					fixupSize, fixupBegin, fixupEnd
				));
				
				ArrayList<LxFixupRecord> fixupRecords = new ArrayList<LxFixupRecord>();
				exe.fixupRecordTable.add(i, fixupRecords);
				int offset = exe.dosHeader.fileAddressNewExe + exe.header.fixupRecordTableOffset + fixupBegin;
				input.getChannel().position(offset);
				while (input.getChannel().position() < offset + fixupSize) {
					LxFixupRecord fixup = new LxFixupRecord();
					leReader.readFixupRecord(fixup, input);
					fixupRecords.add(fixup);
					fixupTotal += fixup.getSize();
				}
			}
			
			System.out.println(String.format("[%x - %x] fixup records table", exe.dosHeader.fileAddressNewExe + exe.header.fixupRecordTableOffset, exe.dosHeader.fileAddressNewExe + exe.header.fixupRecordTableOffset + fixupTotal));
			System.out.println(String.format("[%x - XXX] import module name", exe.dosHeader.fileAddressNewExe + exe.header.importModuleNameTableOffset));
			System.out.println(String.format("[%x - XXX] import proc name", exe.dosHeader.fileAddressNewExe + exe.header.importProcedureNameTableOffset));
			System.out.println(String.format("[%x / %x] preload pages",exe.header.pagesInPreloadSectionCount, exe.header.preloadPagesCount));
			System.out.println(String.format("[%x - XXX] non-res name table offset", exe.header.nameTableOffset));
			System.out.println(String.format("[%x - XXX] res name table offset", exe.header.residentNameTableOffset));
			System.out.println(String.format("[%x / %x] loader section / fixup section sizes ", exe.header.loaderSectionSize, exe.header.fixupSectionSize));
			
			// All parsed here
			
			
			// Insert new pages
			exe.header.pageCount++;
			int objectNumber = 0;
			int newPageNumber = 0;
			// new object table entry
			for (ObjectTableEntry originalObject : exe.objectTable) {
				if (objectNumber == 0) {
					originalObject.pageCount++;
					originalObject.size += exe.header.pageSize;
					newPageNumber = originalObject.pageCount;
				} else {
					originalObject.pageTableIndex++;
				}
				objectNumber++;
			}
			// new page map entry
			LeObjectPageTableEntry newPageEntry = new LeObjectPageTableEntry();
			newPageEntry.dataOffset = newPageNumber;
			newPageEntry.flags = 0;
			exe.objectPageTable.add(newPageNumber-1, newPageEntry);
			
			// new fixup list
			exe.fixupRecordTable.add(newPageNumber-1, new ArrayList<LxFixupRecord>());
			
			// new page
			byte[] newPage = new byte[exe.header.pageSize];
			Arrays.fill(newPage, 0, exe.header.pageSize, (byte)0x41);
			exe.pages.add(newPageNumber-1, newPage);
			System.out.println(String.format("Added page %x", newPageNumber));
			
			if (source == "D:\\Games\\f1mod20\\F2.exe") return;
			
			// Write LE
			int offsetHeader = leOffset;
			int offsetObjectTable = offsetHeader + LxHeader.SIZE;
			int objectCount = exe.objectTable.size();
			int pageCount = exe.header.pageCount;
			
			int offsetObjectPageTable = offsetObjectTable + objectCount * ObjectTableEntry.SIZE;
			
			int offsetFixupPageTable = offsetObjectPageTable + LeObjectPageTableEntry.SIZE * pageCount;
			int offsetFixupRecordsTable = offsetFixupPageTable + 4 * pageCount + 4;
			int fixupRecordsSize = 0;
			for (ArrayList<LxFixupRecord> pageRecords : exe.fixupRecordTable) {
				for (LxFixupRecord fixup : pageRecords) {
					fixupRecordsSize += fixup.getSize();
				}
			}
			int offsetDataPages = offsetFixupRecordsTable + fixupRecordsSize;
			offsetDataPages = (int) (exe.header.pageSize * Math.ceil((double)offsetDataPages / exe.header.pageSize));
			
			for (ObjectTableEntry originalObject : exe.objectTable) {
				System.out.println(String.format("Writing object %x: %x pages", originalObject.number, originalObject.pageCount));
			}
			
			System.out.println(String.format("[%x - %x] DOS EXE", 0, offsetHeader));
			System.out.println(String.format("[%x - %x] LE Header", offsetHeader, offsetHeader + LxHeader.SIZE));
			System.out.println(String.format("[%x - %x] object table (%x objects)", offsetObjectTable, offsetObjectTable + ObjectTableEntry.SIZE * objectCount, objectCount));
			System.out.println(String.format("[%x - %x] object pages table (%x pages)", offsetObjectPageTable, offsetObjectPageTable + LeObjectPageTableEntry.SIZE * pageCount, pageCount));
			System.out.println(String.format("[%x - %x] fixup page table", offsetFixupPageTable, offsetFixupPageTable + 4 * pageCount + 4));
			
			System.out.println(String.format("[%x - %x] fixup records table", offsetFixupRecordsTable, offsetFixupRecordsTable + fixupRecordsSize));
			System.out.println(String.format("[%x - %x] data pages (%x pages)", offsetDataPages, offsetDataPages + pageCount * exe.header.pageSize, pageCount));
			
			
			// Dump it
			
			FileOutputStream output = new FileOutputStream(target);
			// Copy MZ header + DOS code
			input.getChannel().position(0);
			output.write(input.readNBytes(exe.dosHeader.fileAddressNewExe));
			
			LeWriter writer = new LeWriter();
			// header
			exe.header.objectTableOffset = offsetObjectTable - leOffset;
			exe.header.pageTableOffset = offsetObjectPageTable - leOffset;
			exe.header.fixupPageTableOffset = offsetFixupPageTable - leOffset;
			exe.header.fixupRecordTableOffset = offsetFixupRecordsTable - leOffset;
			exe.header.dataPagesOffset = offsetDataPages;
			writer.writeLxHeader(exe.header, output);
			
			// object table
			for (ObjectTableEntry originalObject : exe.objectTable) {
				writer.writeObjectTableEntry(originalObject, output);
			}
			// object page table
			int i = 1;
			for (ObjectPageTableEntry entry : exe.objectPageTable) {
				LeObjectPageTableEntry newEntry = new LeObjectPageTableEntry();
				newEntry.flags = (byte) entry.getFlags();
				newEntry.dataOffset = i++;
				writer.writeObjectPageTableEntry(newEntry, output);
			}
			// fixup page table
			int currentOffset = 0;
			for (ArrayList<LxFixupRecord> pageRecords : exe.fixupRecordTable) {
				writer.writeInt(currentOffset, output);
				for (LxFixupRecord fixup : pageRecords) {
					currentOffset += fixup.getSize();
				}
			}
			writer.writeInt(currentOffset, output);
			// fixup records
			for (ArrayList<LxFixupRecord> pageRecords : exe.fixupRecordTable) {
				for (LxFixupRecord fixup : pageRecords) {
					writer.writeFixupRecord(fixup, output);
				}
			}
			// data pages
			output.getChannel().position(offsetDataPages);
			for (byte[] data : exe.pages) {
				output.write(data);
			}
			
			input.close();
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(String.format("Written output to: %s", target));
    }
}
