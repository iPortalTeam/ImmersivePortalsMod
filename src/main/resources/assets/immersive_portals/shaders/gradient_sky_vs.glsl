#version 120

uniform mat4 modelView;
uniform mat4 projection;

varying vec3 pos;
varying vec4 color;

void main(){
    pos = gl_Vertex.xyz;
    vec4 modelViewed=(modelView * gl_Vertex);
    gl_Position = projection * modelViewed;
    color = gl_Color;
}