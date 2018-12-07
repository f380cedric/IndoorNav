package com.example.francois.indoornav.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UwbFrame {
    private final ByteBuffer buffer = ByteBuffer.allocate(127).order(ByteOrder.LITTLE_ENDIAN);

    private int frameLength;
    public final UwbFrameShort uwbFrameShort = new UwbFrameShort();

    public void setFrame(byte[] src) {
        buffer.clear();
        buffer.put(src);
        frameLength = src.length;

    }

    public byte[] getFrameCopy() {
        byte[] frame = new byte[frameLength];
        ((ByteBuffer)buffer.position(0)).get(frame);
        return frame;
    }

    public byte[] getFrame() {
        return buffer.array();
    }

    public class UwbFrameShort {
        private final ByteBuffer fc = (ByteBuffer) buffer.slice().limit(2);
        private final ByteBuffer seq = (ByteBuffer) ((ByteBuffer)buffer.position(2)).slice().limit(1);
        private final ByteBuffer dstPanId = (ByteBuffer) ((ByteBuffer)buffer.position(3)).slice().limit(2);
        private final ByteBuffer dstAddr = (ByteBuffer) ((ByteBuffer)buffer.position(5)).slice().limit(2);
        private final ByteBuffer srcAddr = (ByteBuffer) ((ByteBuffer)buffer.position(7)).slice().limit(2);
        private final ByteBuffer data = ((ByteBuffer)buffer.position(9)).slice();
        private final int headerLength = 9;
        private int dataLenght;


        public void setData(byte[] data) {
            ((ByteBuffer)this.data.position(0)).put(data);
            dataLenght = data.length;
            frameLength = headerLength + data.length;
        }



        public void setFc(short fc) {
            this.fc.putShort(0, fc);
        }

        public void setSeq(byte seq) {
            this.seq.put(0, seq);
        }

        public void  incSeq(){
            seq.put(0, (byte) (seq.get(0)+1));
        }

        public void setDstPanId(short pandId) {
            dstPanId.putShort(0, pandId);
        }

        public void setDstAddr(short addr) {
            dstAddr.putShort(0,addr);
        }

        public void setSrcAddr(short addr) {
            srcAddr.putShort(0, addr);
        }

        public short getFc() {
            return fc.getShort(0);
        }

        public byte getSeq() {
            return seq.get(0);
        }

        public short getDstPanId() {
            return dstPanId.getShort(0);
        }

        public short getDstAddr() {
            return dstAddr.getShort(0);
        }

        public short getSrcAddr() {
            return srcAddr.getShort(0);
        }

        public byte[] getData() {
            byte[] dataArray = new byte[dataLenght];
            ((ByteBuffer)data.position(0)).get(dataArray);
            return dataArray;
        }

        public int getHeaderLength() {
            return headerLength;
        }

        public int getFrameLength() {
            return frameLength = headerLength + dataLenght;
        }
    }


}
