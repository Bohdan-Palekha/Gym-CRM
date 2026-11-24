package com.gymcrm.model;

import jakarta.persistence.*;
import lombok.*;

//JOINED inheritance, users hold shared columns,
// trainer and trainee store their columns in a separate table whit PK being FK ro users.id
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"password", "rawPassword"})
@EqualsAndHashCode(of = "id")
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    //hashed (salt embedded) at rest - security task 4
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    //not a column
    //hold generated password in-memory between generation and reg response
    @Transient
    private String rawPassword;
}
