#version 120

varying vec3 pos;
varying vec4 color;

uniform float origin;

void main(){
    float width=0.3;

    vec3 normalizedPos=normalize(pos);
    float level=-min(normalizedPos.y-origin, 0);
    float alpha=1-max((level)/(width), 0);

    gl_FragColor = vec4(color.xyz, alpha);
}