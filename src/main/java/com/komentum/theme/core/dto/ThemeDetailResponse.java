package com.komentum.theme.core.dto;

import com.komentum.designcomponent.domain.ComponentType;
import com.komentum.designcomponent.domain.DesignComponent;
import com.komentum.designcomponent.enums.PlatformScope;
import com.komentum.designcomponent.enums.StyleCode;
import com.komentum.designcomponent.enums.TypeCode;
import com.komentum.designcomponent.enums.TypeCodeGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "테마 상세 조회 응답 DTO")
public class ThemeDetailResponse {

  @Schema(description = "테마 식별자")
  private Integer themeComponentId;
  @Schema(description = "테마 이름")
  private String themeName;
  @Schema(description = "typeCode별 이미지 정보")
  Map<TypeCode, TypeCodeInfo> typeCodes;
  @Schema(description = "styleCode별 색상 정보")
  Map<StyleCode, StyleCodeInfo> styleCodes;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "typeCode에 대한 색상 정보")
  public static class TypeCodeInfo {

    @Schema(description = "디자인 에셋 ID")
    Integer designComponentId;
    @Schema(description = "디자인 에셋 이미지 URL")
    String imageUrl;
    @Schema(description = "호환되는 플랫폼 정보", example = "ANDROID | COMMON | IOS")
    PlatformScope platformScope;
    @Schema(description = "이미지 대분류")
    TypeCodeGroup typeCodeGroup;
    @Schema(description = "이미지 대분류 이름")
    String typeCodeGroupName;

    public static TypeCodeInfo of(DesignComponent designComponent, ComponentType componentType) {
      TypeCode typeCode = componentType.getTypeCode();
      return TypeCodeInfo.builder()
          .designComponentId(designComponent.getDesignComponentId())
          .imageUrl(designComponent.getImageUrl())
          .platformScope(componentType.getPlatformScope())
          .typeCodeGroup(typeCode.getTypeCodeGroup())
          .typeCodeGroupName(typeCode.getTypeCodeGroup().getDescription())
          .build();
    }
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StyleCodeInfo {

    String color;
    Integer alpha;
    PlatformScope platformScope;

    public static StyleCodeInfo of(String color, Integer alpha, PlatformScope platformScope) {
      return StyleCodeInfo.builder()
          .color(color)
          .alpha(alpha)
          .platformScope(platformScope)
          .build();
    }
  }
}
