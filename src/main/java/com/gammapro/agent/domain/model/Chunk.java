package com.gammapro.agent.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Chunk {
    private String filename;
    private int index;
    private String text;
    private double[] embedding;
}
