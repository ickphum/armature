#version 310 es
precision mediump float;
in vec4 v_Color;
out vec4 finalColor;
void main()
{
    finalColor = vec4( v_Color );
}
