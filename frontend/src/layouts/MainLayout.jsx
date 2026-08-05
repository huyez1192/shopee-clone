import { Outlet } from "react-router-dom";

function MainLayout() {
  return (
    <div>
      {/* Header và Footer sẽ được thiết kế sau */}
      <Outlet />
    </div>
  );
}

export default MainLayout;