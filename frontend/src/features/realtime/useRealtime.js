import { useContext } from "react";
import RealtimeContext from "./RealtimeContext";

export default function useRealtime() {
  return useContext(RealtimeContext);
}
