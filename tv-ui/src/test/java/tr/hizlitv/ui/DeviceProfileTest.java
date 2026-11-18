package tr.hizlitv.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeviceProfileTest {
    @Test public void videoCapFollowsThePanelAndMemory() {
        assertArrayEquals(new int[]{1920, 1080}, DeviceProfile.maxVideoSize(1920, 1080, true, 900));   // Mi TV Stick class
        assertArrayEquals(new int[]{3840, 2160}, DeviceProfile.maxVideoSize(3840, 2160, false, 3000)); // 4K Google TV class
        assertArrayEquals(new int[]{1920, 1080}, DeviceProfile.maxVideoSize(3840, 2160, true, 1500));  // 4K panel, low-RAM SoC
        assertArrayEquals(new int[]{1920, 1080}, DeviceProfile.maxVideoSize(1280, 720, false, 2000));
    }
    @Test public void bufferGrowsOnlyForFourKPlayback() {
        assertEquals(24 << 20, DeviceProfile.bufferBytes(false, 1990)); // 1080p stick with 2 GB keeps the tested 24 MB
        assertEquals(24 << 20, DeviceProfile.bufferBytes(false, 8000));
        assertEquals(48 << 20, DeviceProfile.bufferBytes(true, 2000));
        assertEquals(96 << 20, DeviceProfile.bufferBytes(true, 4000));
    }
}
