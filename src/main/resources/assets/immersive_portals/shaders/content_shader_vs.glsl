#version 120

uniform mat4 modelView;
uniform mat4 projection;

void main(){
    vec4 modelViewed= (modelView * gl_Vertex);
    gl_Position = projection * modelViewed;
}