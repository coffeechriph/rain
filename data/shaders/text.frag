#version 420 core

layout(location = 0) out vec4 color;
layout(binding = 0) uniform sampler2D texSampler;

layout(location = 0) in vec2 Uv;
layout(location = 1) in vec2 fpos;
layout(location = 2) in vec4 containerData;

void main() {
    if (fpos.x < containerData.x || fpos.x > containerData.x + containerData.z ||
        fpos.y < containerData.y || fpos.y > containerData.y + containerData.w) {
      discard;
    }
    else {
      color.a = texture(texSampler, Uv).r;
      color.rgb = vec3(1.0);

      if (color.a < 0.01f) {
          discard;
      }
    }
}
