package top.codings.websiphon.light.bean;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class M3u8 {
    private byte[] content;
}
