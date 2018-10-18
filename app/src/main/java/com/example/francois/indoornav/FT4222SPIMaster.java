/*package com.example.francois.indoornav;

import android.content.Context;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.ft4222.FT_4222_Spi_Master;

public class FT4222SPIMaster extends FT_4222_Spi_Master {
    D2xxManager manager;
    Context global_context;
    public FT4222SPIMaster(Context context) throws D2xxManager.D2xxException {
        global_context = context;
        manager = D2xxManager.getInstance(global_context);
        int
        manager.createDeviceInfoList(global_context);

        FTDevice mFTDevice0 = m_pFtD2xx.openByIndex(m_pActivity, 0, params);

        if(mFTDevice0 != null)

        {

            mFT4222Device0 = new FT_4222_Device(mFTDevice0);

            mFT4222Device0.init();

            mFT4222Device0.setClock((byte)FT4222_ClockRate.SYS_CLK_80); //80 MHZ

            mSPIMaster = m_FT4222Device0.getSpiMasterDevice();

            mSPIMaster.init(FT4222_SPIMode.SPI_IO_SINGLE, FT4222_SPIClock.CLK_DIV_4, FT4222_SPICPOL.CLK_ACTIVE_LOW, FT4222_SPICPHA.CLK_LEADING, 1);

            mSPIMaster.singleReadWrite(rd_buf, wr_buf, wr_buf.length, sizeTransferred, false);
        }
}*/