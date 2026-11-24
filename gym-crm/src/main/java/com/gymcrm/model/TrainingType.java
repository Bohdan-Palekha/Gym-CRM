package com.gymcrm.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "training_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "trainingTypeName")
public class TrainingType {

    public enum TrainingTypeName {
        FITNESS, YOGA, ZUMBA, STRETCHING, RESISTANCE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_type_name", nullable = false, unique = true)
    private TrainingTypeName trainingTypeName;
}
