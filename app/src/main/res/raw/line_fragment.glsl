#version 310 es
precision mediump float;
uniform vec4 u_Color;
out vec4 finalColor;

void main()
{

    finalColor = u_Color;
}
