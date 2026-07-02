package com.suresell.printagent.printer;

public interface PrinterPort {
    void printBytes(byte[] data);
    void openDrawer();
    boolean isPrinterReady();
}
