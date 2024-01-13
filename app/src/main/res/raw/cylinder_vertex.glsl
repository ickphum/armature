uniform mat4 u_Matrix;
uniform vec3 u_VectorToLight;

attribute vec3 a_Position;
attribute vec3 a_Normal;
varying vec3 v_Color;
void main()
{
    v_Color = vec3( 0.8, 0.3, 0.4 );

    float diffuse = max(dot(a_Normal, u_VectorToLight), 0.0);
    v_Color *= diffuse;
    v_Color += 0.2; // ambient

    gl_Position = u_Matrix * vec4(a_Position, 1.0);
}
