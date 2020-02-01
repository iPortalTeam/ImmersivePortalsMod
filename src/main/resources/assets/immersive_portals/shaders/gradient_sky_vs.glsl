#version 120

varying vec3 pos;
varying vec4 color;

void main(){
    pos = gl_Vertex.xyz;
    vec4 modelViewed=(gl_ModelView * gl_Vertex);
    gl_Position = gl_Projection * modelViewed;
    color = gl_Color;
}