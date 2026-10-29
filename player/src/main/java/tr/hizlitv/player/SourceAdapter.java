package tr.hizlitv.player;

/** An optional, bounded page-format adapter. No script execution or network access. */
interface SourceAdapter {
    boolean supports(String host);
    String extract(String html, String host) throws Exception;
}
