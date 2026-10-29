package tr.hizlitv.media;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
public class LibraryQueryTest {
    @Test public void combinesFiltersAndHandlesTurkishCaseWithoutMutatingLibrary() {
        MediaEntry film = new MediaEntry(); film.title="İstanbul"; film.group="Filmler"; film.favorite=true;
        MediaEntry channel = new MediaEntry(); channel.title="Haber"; channel.group="Canlı";
        List<MediaEntry> all=Arrays.asList(film,channel);
        assertEquals(1,LibraryQuery.filter(all,true,"Filmler","istanbul").size());
        assertEquals(0,LibraryQuery.filter(all,true,"Canlı","").size());
        assertEquals(2,all.size()); assertEquals("İstanbul",film.title);
        assertEquals(2,LibraryQuery.filter(all,false,"","").size());
    }
}
