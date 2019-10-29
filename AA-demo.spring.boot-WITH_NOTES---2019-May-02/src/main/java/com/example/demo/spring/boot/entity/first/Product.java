package com.example.demo.spring.boot.entity.first;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "PRODUCT")
@Getter
@Setter
public class Product {

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = "prodSeq", sequenceName = "PRODUCT_SEQUENCE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prodSeq")
    private int id;

    @Column(name = "TITLE")
    private String title;
}
