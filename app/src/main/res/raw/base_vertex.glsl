uniform mat4 u_Matrix;
attribute vec3 a_Position;
void main()
{
    gl_Position = u_Matrix * vec4(a_Position, 1.0);
}
