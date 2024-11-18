package com.ele.crawler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FbPostDto {

    private String text;           
    private String imageUrl;       
    private String likes;          
    private String comments;       
    private String shares;         
}
