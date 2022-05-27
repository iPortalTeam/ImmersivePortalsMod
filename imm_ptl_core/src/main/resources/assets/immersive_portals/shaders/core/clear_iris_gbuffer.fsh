#version 150

in vec4 vertexColor;

uniform vec4 clearingColor;

// currently Iris only support 8 gbuffers
layout (location = 0) out vec4 iris_FragData[8];

void main() {
//    vec4 color = vertexColor;

    iris_FragData[0] = clearingColor;
    iris_FragData[1] = clearingColor;
    iris_FragData[2] = clearingColor;
    iris_FragData[3] = clearingColor;
    iris_FragData[4] = clearingColor;
    iris_FragData[5] = clearingColor;
    iris_FragData[6] = clearingColor;
    iris_FragData[7] = clearingColor;
}
