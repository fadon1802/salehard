package org.uroran.models;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class SessionData {
    private String name;
    private String host;
    private int port;
    private String user;
    private String pathToKey;
    private String passPhrase;
}
