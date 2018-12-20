#version 420 core

layout(location = 0) in vec3 pos;

layout(set = 1, binding = 0) uniform TextureData {
  vec2 uvScale;
} textureData;

layout(push_constant) uniform ModelMatrix {
    mat4 matrix;
    vec2 velocity;
    float time;
    float particleLifetime;
    float particleSize;
} inData;

layout(set = 0, binding = 0) uniform SceneData {
    mat4 projectionMatrix;
} sceneData;

layout (location = 0) out float alpha;

void main() {
    float ftime = inData.time + pos.z;
    ftime = mod(ftime, inData.particleLifetime);
    alpha = (inData.particleLifetime - ftime) / inData.particleLifetime;

    float px = pos.x + inData.velocity.x * ftime;
    float py = pos.y + inData.velocity.y * ftime;
    float scale = inData.particleSize * (1.0 - alpha);
    px *= scale;
    py *= scale;
    gl_Position = sceneData.projectionMatrix * inData.matrix * vec4(px, py, 1.0, 1.0);
}
