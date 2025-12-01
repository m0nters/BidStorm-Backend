package com.taitrinh.online_auction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @Column(nullable = false)
    private Short id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    // Role constants
    public static final short ADMIN = 1;
    public static final short SELLER = 2;
    public static final short BIDDER = 3;
}
