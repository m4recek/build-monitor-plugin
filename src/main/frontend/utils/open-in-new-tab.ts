export default function openInNewTab(destination: string) {
  // Open after the click event so its Shift modifier cannot request a window.
  window.setTimeout(() => window.open(destination, "_blank", "noopener"), 0);
}
