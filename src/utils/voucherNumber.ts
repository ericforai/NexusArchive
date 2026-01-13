const VOUCHER_PREFIX_REGEX = /^[记收付转资产]/;

export interface FormatVoucherNumberOptions {
  displayValue?: string;
  voucherWord?: string;
  voucherNo?: string;
  fallback?: string;
}

export const formatVoucherNumber = (options: FormatVoucherNumberOptions): string => {
  const displayValue = options.displayValue?.trim();
  if (displayValue) return displayValue;

  const voucherNo = options.voucherNo?.trim();
  const voucherWord = options.voucherWord?.trim();

  if (voucherNo) {
    // 如果 voucherNo 已经包含凭证字（如 "记-8"）直接使用
    if (VOUCHER_PREFIX_REGEX.test(voucherNo)) {
      return voucherNo;
    }
    if (voucherWord) {
      return `${voucherWord}-${voucherNo}`;
    }
    return voucherNo;
  }

  if (voucherWord) {
    return voucherWord;
  }

  return options.fallback?.trim() || '-';
};
