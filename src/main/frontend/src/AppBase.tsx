import {FunctionComponent} from "react";
import {StaticPlusConfig} from "./index";
import StaticPlusIndex from "./components/StaticPlusIndex";

export type AppBaseProps = {
    config: StaticPlusConfig;
}

const AppBase: FunctionComponent<AppBaseProps> = ({config}) => {
    return <StaticPlusIndex config={config} />;
}

export default AppBase;
