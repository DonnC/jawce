package zw.co.dcl.engine.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "config")
public class ConfigEntity implements Serializable {
    @Id
    @GeneratedValue
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String accessToken;
    private String hubToken;
    private String phoneNumberId;
    private String apiVersion = "v18.0";
}
