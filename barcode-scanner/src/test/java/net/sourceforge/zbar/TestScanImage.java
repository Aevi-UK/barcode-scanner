package net.sourceforge.zbar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.RandomAccessFile;
import java.util.Iterator;

import static org.junit.Assert.*;

public class TestScanImage {
    protected ImageScanner scanner;
    protected Image image;

    @Before
    public void setUp() {
        scanner = new ImageScanner();
        image = new Image();
    }

    @After
    public void tearDown() {
        image = null;
        scanner = null;
        System.gc();
    }

    protected void checkResults(SymbolSet syms) {
        checkResults(syms, Orientation.RIGHT);
    }

    protected void checkResults(SymbolSet syms, int orientation) {
        assertNotNull(syms);
        assert (syms.size() == 1);
        Iterator<Symbol> it = syms.iterator();
        assertTrue(it.hasNext());
        Symbol sym = it.next();
        assertNotNull(sym);
        assertFalse(it.hasNext());

        assertEquals(Symbol.QRCODE, sym.getType());
        assertEquals(sym.QRCODE, sym.getType()); // cached

        // Note: quality for QR codes always seem to be one
        assertTrue(sym.getQuality() == 1);
        assertEquals(0, sym.getCount());

        SymbolSet comps = sym.getComponents();
        assertNotNull(comps);
        assertEquals(0, comps.size());
        it = comps.iterator();
        assertNotNull(it);
        assertFalse(it.hasNext());

        String data = sym.getData();
        assertEquals("Hello World from QR code", data);

        assertEquals(orientation, sym.getOrientation());
    }

    protected void generateY800() {
        image.setSize(1280, 720);
        image.setFormat("Y800");
        try (RandomAccessFile access = new RandomAccessFile("src/test/y800-1280x720.bin", "r")) {
            byte[] buffer = new byte[(int) access.length()];
            access.readFully(buffer);
            image.setData(buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void generated() {
        generateY800();
        int n = scanner.scanImage(image);
        assertEquals(1, n);

        checkResults(image.getSymbols());
        checkResults(scanner.getResults());
    }

    @Test
    public void config() {
        generateY800();
        scanner.setConfig(Symbol.QRCODE, Config.ENABLE, 0);
        int n = scanner.scanImage(image);
        assertEquals(0, n);
    }

    @Test
    public void orientation() throws Exception {
        generateY800();

        // flip the image
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] data = image.getData();
        int p = 0;
        for (int y = 0; y < height; y++) {
            for (int x0 = 0; x0 < width / 2; x0++) {
                int x1 = width - x0 - 1;
                assert (x0 < x1);
                byte b = data[p + x0];
                data[p + x0] = data[p + x1];
                data[p + x1] = b;
            }
            p += width;
        }
        image.setData(data);

        int n = scanner.scanImage(image);
        assertEquals(1, n);

        checkResults(scanner.getResults(), Orientation.LEFT);
    }
}
