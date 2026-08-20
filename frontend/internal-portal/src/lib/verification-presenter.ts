import type { VerificationCheck, VerificationReport } from "@/types/verification";

type CheckNarrativeDefinition = {
  title: string;
  success: string;
  failure: string;
  passCondition: string;
  failCondition: string;
  decidingService: string;
};

type VerificationSignal = {
  key: string;
  title: string;
  result: boolean;
  message: string;
};

const CHECK_NARRATIVES: Record<string, CheckNarrativeDefinition> = {
  package_loaded: {
    title: "Hệ thống đã nhận được tệp cần xác minh",
    success: "Tệp tải lên đã được nhận và đưa vào quy trình kiểm tra.",
    failure: "Hệ thống không đọc được tệp tải lên để bắt đầu xác minh.",
    passCondition: "Tệp đầu vào được đọc thành công và chuyển vào luồng xác minh.",
    failCondition: "Tệp rỗng, không đọc được hoặc lỗi khi tải nội dung vào bộ xác minh.",
    decidingService: "verification-service",
  },
  signature_container_detected: {
    title: "Hệ thống nhận diện được kiểu container chữ ký",
    success: "Hệ thống đã nhận diện được định dạng signed output của tệp.",
    failure: "Hệ thống không nhận diện được container chữ ký của tệp này.",
    passCondition: "Tệp được nhận diện là PAdES PDF, CMS/CAdES hoặc gói ZIP ký theo định dạng cũ.",
    failCondition: "Không xác định được định dạng chữ ký hoặc định dạng không được hỗ trợ.",
    decidingService: "verification-service",
  },
  pdf_signature_present: {
    title: "PDF có chứa chữ ký số nhúng bên trong",
    success: "Tài liệu PDF có chứa vùng chữ ký số nhúng trong tệp.",
    failure: "Không tìm thấy chữ ký số nhúng bên trong tài liệu PDF.",
    passCondition: "PDF có ít nhất một `PDSignature` trong cấu trúc tài liệu.",
    failCondition: "PDF không có trường chữ ký số hoặc không đọc ra được chữ ký nhúng.",
    decidingService: "verification-service",
  },
  pdf_signature_parsed: {
    title: "Hệ thống đọc được cấu trúc PAdES trong PDF",
    success: "Cấu trúc chữ ký PAdES trong PDF đã được phân tích thành công.",
    failure: "Hệ thống không đọc được cấu trúc chữ ký PAdES trong PDF.",
    passCondition: "CMS nhúng trong PDF được parse thành công và trích ra signer/certificate.",
    failCondition: "Không parse được CMS/PAdES hoặc cấu trúc chữ ký trong PDF bị lỗi.",
    decidingService: "verification-service",
  },
  cades_attributes_present: {
    title: "Bộ thuộc tính ký số của tài liệu",
    success: "Bộ thuộc tính ký số theo hướng CAdES/PAdES đã hiện diện.",
    failure: "Bộ thuộc tính ký số theo hướng CAdES/PAdES đang thiếu hoặc không đầy đủ.",
    passCondition: "Các thuộc tính bắt buộc như messageDigest, signingTime, signingCertificateV2 xuất hiện đầy đủ.",
    failCondition: "Thiếu một hoặc nhiều thuộc tính ký số cần thiết trong signer info.",
    decidingService: "verification-service",
  },
  document_digest_matches: {
    title: "Nội dung tài liệu so với bản được ký",
    success: "Digest trong chữ ký vẫn khớp với nội dung hiện tại của tài liệu.",
    failure: "Digest trong chữ ký không còn khớp với nội dung hiện tại của tài liệu.",
    passCondition: "Giá trị băm tính lại từ nội dung hiện tại trùng với messageDigest trong chữ ký.",
    failCondition: "Nội dung tài liệu đã thay đổi hoặc messageDigest trong chữ ký không còn khớp.",
    decidingService: "verification-service",
  },
  signature_value_valid: {
    title: "Giá trị chữ ký số",
    success: "Chữ ký số vẫn hợp lệ đối với nội dung hiện tại của tài liệu.",
    failure: "Chữ ký số không còn hợp lệ đối với nội dung hiện tại của tài liệu.",
    passCondition: "SignerInformation xác minh thành công bằng khóa công khai của chứng thư nhúng.",
    failCondition: "Giá trị chữ ký không xác minh được hoặc chữ ký đã bị sửa đổi.",
    decidingService: "verification-service",
  },
  certificate_recognized_by_authority: {
    title: "Chứng thư người ký trong hệ thống authority",
    success: "Authority xác nhận chứng thư nhúng trong tệp là chứng thư đã biết.",
    failure: "Authority không tìm thấy chứng thư nhúng trong tệp để đối chiếu.",
    passCondition: "verification-service tra được metadata chứng thư từ certification-service theo serial hoặc certificateId.",
    failCondition: "Không tra được metadata chứng thư hoặc certification-service trả về lỗi / không tìm thấy.",
    decidingService: "verification-service + certification-service",
  },
  certificate_pem_matches_authority: {
    title: "Độ khớp của chứng thư nhúng với bản ghi authority",
    success: "Nội dung PEM của chứng thư nhúng trong tệp khớp với dữ liệu authority đang quản lý.",
    failure: "Nội dung PEM của chứng thư nhúng trong tệp không được authority xác nhận.",
    passCondition: "PEM, serial và subject DN của chứng thư nhúng đều khớp với metadata từ authority.",
    failCondition: "PEM, serial hoặc subject DN khác với dữ liệu authority trả về.",
    decidingService: "verification-service + certification-service",
  },
  certificate_time_valid: {
    title: "Thời hạn hiệu lực của chứng thư người ký",
    success: "Chứng thư người ký nằm trong khoảng hiệu lực tại thời điểm được đánh giá.",
    failure: "Chứng thư người ký nằm ngoài khoảng hiệu lực tại thời điểm được đánh giá.",
    passCondition: "Thời điểm đánh giá nằm trong khoảng `validFrom` đến `validTo` của chứng thư.",
    failCondition: "Thời điểm đánh giá nằm trước `validFrom` hoặc sau `validTo`.",
    decidingService: "verification-service + certification-service",
  },
  certificate_not_revoked: {
    title: "Trạng thái thu hồi của chứng thư người ký",
    success: "Hệ thống xác nhận chứng thư người ký chưa bị thu hồi.",
    failure: "Hệ thống không xác nhận được chứng thư người ký chưa bị thu hồi.",
    passCondition: "Authority cho biết chứng thư chưa bị thu hồi, hoặc bị thu hồi nhưng sau thời điểm ký tin cậy.",
    failCondition: "Không tra được trạng thái thu hồi, hoặc chứng thư đã bị thu hồi tại / trước thời điểm đánh giá.",
    decidingService: "verification-service + certification-service",
  },
  trusted_timestamp_present: {
    title: "Timestamp token đi kèm tài liệu",
    success: "Tài liệu có kèm RFC 3161 timestamp token để chứng minh mốc thời gian ký.",
    failure: "Tài liệu không kèm RFC 3161 timestamp token.",
    passCondition: "Unsigned attributes của signer có thuộc tính `id_aa_signatureTimeStampToken`.",
    failCondition: "Không có timestamp token trong unsigned attributes.",
    decidingService: "verification-service",
  },
  timestamp_message_imprint_matches_signature: {
    title: "Sự liên kết giữa timestamp token và chữ ký số",
    success: "Message imprint trong timestamp token khớp với bytes chữ ký số của tài liệu.",
    failure: "Message imprint trong timestamp token không khớp với bytes chữ ký số của tài liệu.",
    passCondition: "Digest của bytes chữ ký số trùng với message imprint trong timestamp token.",
    failCondition: "Timestamp token không thực sự đóng dấu cho chữ ký số hiện có trong tài liệu.",
    decidingService: "verification-service",
  },
  timestamp_token_signature_valid: {
    title: "Giá trị chữ ký của timestamp token",
    success: "Timestamp token đã được ký hợp lệ bởi timestamp authority.",
    failure: "Chữ ký của timestamp token không hợp lệ.",
    passCondition: "Timestamp token xác minh thành công bằng chứng thư TSA nhúng trong token.",
    failCondition: "Không xác minh được chữ ký của token hoặc token bị sửa đổi.",
    decidingService: "verification-service",
  },
  timestamp_authority_recognized: {
    title: "Timestamp authority trong hệ thống trust",
    success: "Hệ thống tìm thấy timestamp authority tương ứng với token này.",
    failure: "Hệ thống không tìm thấy timestamp authority tương ứng với token này.",
    passCondition: "verification-service tra được TSA metadata từ certification-service theo serial của TSA.",
    failCondition: "Không tra được TSA metadata hoặc certification-service không nhận ra serial của TSA.",
    decidingService: "verification-service + certification-service",
  },
  timestamp_authority_matches_registry: {
    title: "Độ khớp của timestamp authority với registry",
    success: "Chứng thư của timestamp authority khớp với bản ghi trust hiện có.",
    failure: "Chứng thư của timestamp authority không được registry trust xác nhận.",
    passCondition: "PEM, serial và subject DN của TSA nhúng khớp với TSA metadata trong registry.",
    failCondition: "PEM, serial hoặc subject DN của TSA không khớp với dữ liệu registry.",
    decidingService: "verification-service + certification-service",
  },
  timestamp_authority_time_valid: {
    title: "Thời hạn hiệu lực của timestamp authority",
    success: "Chứng thư của timestamp authority nằm trong khoảng hiệu lực tại lúc token được tạo.",
    failure: "Chứng thư của timestamp authority nằm ngoài khoảng hiệu lực tại lúc token được tạo.",
    passCondition: "TSA ở trạng thái hoạt động và `generatedAt` nằm trong khoảng hiệu lực của chứng thư TSA.",
    failCondition: "TSA không còn hoạt động, hoặc token được tạo ngoài khoảng hiệu lực của chứng thư TSA.",
    decidingService: "verification-service + certification-service",
  },
  timestamp_authority_not_revoked: {
    title: "Trạng thái thu hồi của timestamp authority",
    success: "Hệ thống xác nhận timestamp authority chưa bị thu hồi.",
    failure: "Hệ thống không xác nhận được timestamp authority chưa bị thu hồi.",
    passCondition: "TSA chưa có `revokedAt`, hoặc `revokedAt` xảy ra sau thời điểm tạo token.",
    failCondition: "Không tra được trạng thái TSA, hoặc TSA đã bị thu hồi tại / trước thời điểm tạo token.",
    decidingService: "verification-service + certification-service",
  },
  trusted_timestamp_validated: {
    title: "Mức độ trust của timestamp token",
    success: "Timestamp token đã được trust đầy đủ và có thể dùng làm mốc thời gian ký tin cậy.",
    failure: "Timestamp token có tồn tại nhưng chưa được trust đầy đủ.",
    passCondition: "Message imprint đúng, chữ ký token hợp lệ, TSA được nhận diện, khớp registry, còn hiệu lực và chưa bị thu hồi.",
    failCondition: "Một hoặc nhiều điều kiện trust của timestamp token không đạt.",
    decidingService: "verification-service + certification-service",
  },
  external_trust_anchor_available: {
    title: "Trust anchor cho CA bên ngoài",
    success: "Hệ thống đã có trust anchor để bắt đầu xây dựng trust chain cho CA bên ngoài.",
    failure: "Hệ thống chưa có trust anchor phù hợp cho CA bên ngoài này.",
    passCondition: "Có trust anchor trong JVM trust store hoặc trong các tệp PEM/CRT người dùng tải lên.",
    failCondition: "Không tìm được trust anchor phù hợp cho chuỗi chứng thư bên ngoài.",
    decidingService: "verification-service",
  },
  external_trust_chain_validated: {
    title: "Trust chain của chứng thư CA bên ngoài",
    success: "Trust chain của chứng thư bên ngoài đã được xây dựng thành công tới một CA được tin cậy.",
    failure: "Không xây dựng được trust chain hợp lệ tới một CA được tin cậy.",
    passCondition: "Chuỗi chứng thư bên ngoài dựng được tới một trust anchor hợp lệ.",
    failCondition: "Chuỗi chứng thư đứt, thiếu intermediate hoặc không nối được tới trust anchor.",
    decidingService: "verification-service",
  },
  external_certificate_not_revoked: {
    title: "Trạng thái thu hồi của chứng thư CA bên ngoài",
    success: "Hệ thống xác nhận chứng thư CA bên ngoài chưa bị thu hồi.",
    failure: "Hệ thống không xác nhận được trạng thái thu hồi của chứng thư CA bên ngoài.",
    passCondition: "Kiểm tra revocation của chứng thư ngoài trả về an toàn.",
    failCondition: "Không xác nhận được trạng thái thu hồi hoặc chứng thư ngoài đã bị thu hồi.",
    decidingService: "verification-service",
  },
};

function humanizeCheckName(checkName: string) {
  return checkName
    .split("_")
    .filter(Boolean)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1).toLowerCase())
    .join(" ");
}

export function describeVerificationCheck(check: VerificationCheck) {
  const narrative = CHECK_NARRATIVES[check.checkName];
  return {
    title: narrative?.title || `Kiểm tra kỹ thuật: ${humanizeCheckName(check.checkName)}`,
    summary:
      check.result
        ? narrative?.success || "Kiểm tra này đã đạt."
        : narrative?.failure || "Kiểm tra này không đạt.",
    technicalDetail: check.detailMessage,
    technicalKey: check.checkName,
    passCondition: narrative?.passCondition || "Điều kiện pass chưa được mô tả riêng cho check này.",
    failCondition: narrative?.failCondition || "Điều kiện fail chưa được mô tả riêng cho check này.",
    decidingService: narrative?.decidingService || "verification-service",
  };
}

export function buildVerificationSignals(report: VerificationReport): VerificationSignal[] {
  return [
    {
      key: "integrity",
      title: "Nội dung tài liệu so với bản được ký",
      result: report.integrityValid,
      message: report.integrityValid
        ? "Nội dung tài liệu hiện tại vẫn toàn vẹn và khớp với bản đã được ký."
        : "Nội dung tài liệu hiện tại không còn khớp với bản đã được ký.",
    },
    {
      key: "signature",
      title: "Giá trị chữ ký số",
      result: report.signatureValid,
      message: report.signatureValid
        ? "Chữ ký số vẫn hợp lệ đối với nội dung hiện tại của tài liệu."
        : "Chữ ký số không còn hợp lệ đối với nội dung hiện tại của tài liệu.",
    },
    {
      key: "trust-chain",
      title: "Trust chain của chứng thư",
      result: report.trustChainValid,
      message: report.trustChainValid
        ? "Trust chain của chứng thư đã được xác nhận."
        : "Trust chain của chứng thư chưa được xác nhận đầy đủ.",
    },
    {
      key: "expiration",
      title: "Thời hạn hiệu lực của chứng thư",
      result: report.expirationValid,
      message: report.expirationValid
        ? "Chứng thư liên quan vẫn nằm trong thời hạn hợp lệ tại thời điểm đánh giá."
        : "Chứng thư liên quan nằm ngoài thời hạn hợp lệ tại thời điểm đánh giá.",
    },
    {
      key: "revocation",
      title: "Trạng thái thu hồi",
      result: report.revocationValid,
      message: report.revocationValid
        ? "Trạng thái thu hồi đã được xác nhận là an toàn."
        : "Trạng thái thu hồi chưa được xác nhận đầy đủ.",
    },
  ];
}
