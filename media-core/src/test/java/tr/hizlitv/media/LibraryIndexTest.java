package tr.hizlitv.media;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class LibraryIndexTest {
    @Test public void largePlaylistCombinesFiltersAndKeepsOrder() {
        List<MediaEntry> entries = new ArrayList<>();
        for (int i=0; i<5000; i++) { MediaEntry e=new MediaEntry(); e.title="İstanbul " + i;
            e.group=i%2==0?"Haber":"Sinema"; e.favorite=i%10==0; entries.add(e); }
        LibraryIndex index=new LibraryIndex(entries); entries.clear();
        assertEquals(5000,index.entries().size());
        assertEquals(500,index.filter(true,"Haber"," İSTANBUL ").size());
        assertEquals(0,index.filter(true,"Sinema","").size());
        assertEquals("İstanbul 10",index.filter(true,"Haber","").get(1).title);
        assertEquals(new TreeSet<>(Arrays.asList("Haber","Sinema")),index.groups());
    }
    @Test public void replacingSnapshotRebuildsSearchAndFavorites() {
        MediaEntry a=new MediaEntry();a.title="Işık";
        LibraryIndex first=new LibraryIndex(Collections.singletonList(a));
        assertEquals(1,first.filter(false,"","ışık").size());
        MediaEntry b=new MediaEntry();b.title="Yeni film";b.favorite=true;
        LibraryIndex next=new LibraryIndex(Collections.singletonList(b));
        assertEquals(0,next.filter(false,"","ışık").size());
        assertEquals(1,next.filter(true,"","yeni").size());
        assertEquals(1,first.filter(false,"","ışık").size());
    }
}
