/*
 * Copyright (c) 2013-2014, Neuro4j.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neuro4j.studio.debug.ui.views;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.model.IInternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.jface.viewers.TreePath;

/**
 * This class is public so the test suite has access - it should be default protection.
 * 
 * @since 3.3
 */
public class ChildrenUpdate extends ViewerUpdateMonitor implements IChildrenUpdate {

    private Object[] fElements;
    private int fIndex;
    private int fLength;

    /**
     * Constructs a request to update an element
     * 
     * @param provider
     *        the content provider
     * @param viewerInput
     *        the current input
     * @param elementPath
     *        the path to the element being update
     * @param element
     *        the element
     * @param index
     *        the index of the element
     * @param elementContentProvider
     *        the content provider for the element
     */
    public ChildrenUpdate(TreeModelContentProvider provider, Object viewerInput, TreePath elementPath, Object element, int index, IElementContentProvider elementContentProvider) {
        super(provider, viewerInput, elementPath, element, elementContentProvider, provider.getPresentationContext());
        fIndex = index;
        fLength = 1;
    }

    public ChildrenUpdate(TreeModelContentProvider provider, Object viewerInput, TreePath elementPath, Object element, int index, int length, IElementContentProvider elementContentProvider) {
        super(provider, viewerInput, elementPath, element, elementContentProvider, provider.getPresentationContext());
        fIndex = index;
        fLength = length;
    }

    protected void performUpdate(boolean updateFilterOnly) {
        TreeModelContentProvider provider = getContentProvider();
        TreePath elementPath = getElementPath();
        if (fElements != null) {
            IInternalTreeModelViewer viewer = provider.getViewer();
            for (int i = 0; i < fElements.length; i++) {
                int modelIndex = fIndex + i;
                Object element = fElements[i];
                if (element != null) {
                    int viewIndex = provider.modelToViewIndex(elementPath, modelIndex);
                    if (provider.shouldFilter(elementPath, element)) {
                        if (provider.addFilteredIndex(elementPath, modelIndex, element)) {
                            if (!updateFilterOnly) {
                                if (DebugUIPlugin.DEBUG_CONTENT_PROVIDER && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(getPresentationContext())) {
                                    DebugUIPlugin.trace("REMOVE(" + getElement() + ", modelIndex: " + modelIndex + " viewIndex: " + viewIndex + ", " + element + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                                }
                                viewer.remove(elementPath, viewIndex);
                            }
                        }
                    } else {
                        if (provider.isFiltered(elementPath, modelIndex)) {
                            provider.clearFilteredChild(elementPath, modelIndex);
                            if (!updateFilterOnly) {
                                int insertIndex = provider.modelToViewIndex(elementPath, modelIndex);
                                if (DebugUIPlugin.DEBUG_CONTENT_PROVIDER) {
                                    DebugUIPlugin.trace("insert(" + getElement() + ", modelIndex: " + modelIndex + " insertIndex: " + insertIndex + ", " + element + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                                }
                                viewer.insert(elementPath, element, insertIndex);
                            }
                        } else if (!updateFilterOnly) {
                            if (DebugUIPlugin.DEBUG_CONTENT_PROVIDER && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(getPresentationContext())) {
                                DebugUIPlugin.trace("replace(" + getElement() + ", modelIndex: " + modelIndex + " viewIndex: " + viewIndex + ", " + element + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                            }
                            viewer.replace(elementPath, viewIndex, element);
                        }
                        if (!updateFilterOnly) {
                            TreePath childPath = elementPath.createChildPath(element);
                            provider.updateHasChildren(childPath);
                            provider.getStateTracker().restorePendingStateOnUpdate(childPath, modelIndex, false, false, false);
                        }
                    }
                }
            }

            if (!updateFilterOnly) {
                provider.getStateTracker().restorePendingStateOnUpdate(elementPath, -1, true, true, true);
            }
        } else if (!updateFilterOnly) {
            provider.updateHasChildren(elementPath);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.ui.viewers.AsynchronousRequestMonitor#performUpdate()
     */
    protected void performUpdate() {
        performUpdate(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate#setChild(java.lang.Object, int)
     */
    public void setChild(Object child, int index) {
        if (fElements == null) {
            fElements = new Object[fLength];
        }
        fElements[index - fIndex] = child;
    }

    /*
     * (non-Javadoc)
     * 
     * This method is public so the test suite has access - it should be default protection.
     * 
     * @see
     * org.eclipse.debug.internal.ui.viewers.model.ViewerUpdateMonitor#coalesce(org.eclipse.debug.internal.ui.viewers
     * .model.ViewerUpdateMonitor)
     */
    public synchronized boolean coalesce(ViewerUpdateMonitor request) {
        if (request instanceof ChildrenUpdate) {
            ChildrenUpdate cu = (ChildrenUpdate) request;
            if (getElement().equals(cu.getElement()) && getElementPath().equals(cu.getElementPath())) {
                int end = fIndex + fLength;
                int otherStart = cu.getOffset();
                int otherEnd = otherStart + cu.getLength();
                if ((otherStart >= fIndex && otherStart <= end) || (otherEnd >= fIndex && otherEnd <= end)) {
                    // overlap
                    fIndex = Math.min(fIndex, otherStart);
                    end = Math.max(end, otherEnd);
                    fLength = end - fIndex;
                    if (DebugUIPlugin.DEBUG_CONTENT_PROVIDER && DebugUIPlugin.DEBUG_TEST_PRESENTATION_ID(getPresentationContext())) {
                        DebugUIPlugin.trace("coalesced: " + this.toString()); //$NON-NLS-1$
                    }
                    return true;
                }
            }
        }
        return false;
    }

    boolean containsUpdate(TreePath path) {
        return getElementPath().equals(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate#getLength()
     */
    public int getLength() {
        return fLength;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate#getOffset()
     */
    public int getOffset() {
        return fIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.ViewerUpdateMonitor#startRequest()
     */
    void startRequest() {
        getElementContentProvider().update(new IChildrenUpdate[] { this });
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("IChildrenUpdate: "); //$NON-NLS-1$
        buf.append(getElement());
        buf.append(" {"); //$NON-NLS-1$
        buf.append(getOffset());
        buf.append("->"); //$NON-NLS-1$
        buf.append(getOffset() + getLength() - 1);
        buf.append("}"); //$NON-NLS-1$
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.ViewerUpdateMonitor#getPriority()
     */
    int getPriority() {
        return 3;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.debug.internal.ui.viewers.model.ViewerUpdateMonitor#getSchedulingPath()
     */
    TreePath getSchedulingPath() {
        return getElementPath();
    }

    /**
     * Sets this request's offset. Used when modifying a waiting request when
     * the offset changes due to a removed element.
     * 
     * @param offset
     *        new offset
     */
    void setOffset(int offset) {
        fIndex = offset;
    }

    Object[] getElements() {
        return fElements;
    }

    protected boolean doEquals(ViewerUpdateMonitor update) {
        return update instanceof ChildrenUpdate &&
                ((ChildrenUpdate) update).getOffset() == getOffset() &&
                ((ChildrenUpdate) update).getLength() == getLength() &&
                getViewerInput().equals(update.getViewerInput()) &&
                getElementPath().equals(update.getElementPath());
    }

    protected int doHashCode() {
        return (int) Math.pow(
                (getClass().hashCode() + getViewerInput().hashCode() + getElementPath().hashCode()) * (getOffset() + 2),
                getLength() + 2);
    }

}
